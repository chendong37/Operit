#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
release_dir="$(cd "${script_dir}/.." && pwd)"
repo_dir="$(git -C "${release_dir}" rev-parse --show-toplevel)"
release_relative_dir="${release_dir#"${repo_dir}/"}"
version="0.1.0-internal.1"
archive_name="zhixing-ai-source-overlay-${version}.zip"
dist_dir="${release_dir}/dist"
source_mode="head"

usage() {
    cat <<'EOF'
Usage: package_source_overlay.sh [--head|--working-tree]

  --head          Package the committed BASE_COMMIT..HEAD snapshot (default).
                  Uncommitted and untracked files are ignored.
  --working-tree  Package BASE_COMMIT..current working tree, including
                  non-ignored untracked files.
EOF
}

if (( $# > 1 )); then
    usage >&2
    exit 2
fi
if (( $# == 1 )); then
    case "$1" in
        --head)
            source_mode="head"
            ;;
        --working-tree)
            source_mode="working-tree"
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            usage >&2
            exit 2
            ;;
    esac
fi

current_head="$(git -C "${repo_dir}" rev-parse --verify HEAD^{commit})"
base_commit_relative_path="${release_relative_dir}/BASE_COMMIT"
if [[ "${source_mode}" == "head" ]]; then
    if ! git -C "${repo_dir}" cat-file -e "${current_head}:${base_commit_relative_path}" 2>/dev/null; then
        printf '%s is not committed at HEAD; commit the release sources or use --working-tree.\n' \
            "${base_commit_relative_path}" >&2
        exit 1
    fi
    base_commit="$(git -C "${repo_dir}" show "${current_head}:${base_commit_relative_path}" | tr -d '[:space:]')"
else
    base_commit="$(tr -d '[:space:]' < "${release_dir}/BASE_COMMIT")"
fi

git -C "${repo_dir}" rev-parse --verify "${base_commit}^{commit}" >/dev/null
if ! git -C "${repo_dir}" merge-base --is-ancestor "${base_commit}" "${current_head}"; then
    printf 'BASE_COMMIT %s is not an ancestor of HEAD %s.\n' "${base_commit}" "${current_head}" >&2
    exit 1
fi

stage_parent="$(mktemp -d)"
trap 'test -n "${stage_parent:-}" && test -d "${stage_parent}" && find "${stage_parent}" -depth -delete' EXIT
stage_name="zhixing-ai-source-overlay-${version}"
stage_dir="${stage_parent}/${stage_name}"
mkdir -p "${stage_dir}/files" "${dist_dir}"

copy_release_metadata() {
    local file_name="$1"
    local relative_path="${release_relative_dir}/${file_name}"
    local destination="${stage_dir}/${file_name}"

    if [[ "${source_mode}" == "head" ]]; then
        if ! git -C "${repo_dir}" cat-file -e "${current_head}:${relative_path}" 2>/dev/null; then
            printf 'Required release file is missing from HEAD: %s\n' "${relative_path}" >&2
            exit 1
        fi
        git -C "${repo_dir}" cat-file blob "${current_head}:${relative_path}" > "${destination}"
    else
        if [[ ! -f "${repo_dir}/${relative_path}" ]]; then
            printf 'Required release file is missing from the working tree: %s\n' "${relative_path}" >&2
            exit 1
        fi
        cp -p -- "${repo_dir}/${relative_path}" "${destination}"
    fi
}

copy_release_metadata "BASE_COMMIT"
copy_release_metadata "README.md"
copy_release_metadata "BUILD_STATUS.md"

declare -A copy_paths_set=()
declare -A delete_paths_set=()

is_generated_path() {
    local relative_path="$1"
    case "${relative_path}" in
        customization/*/dist|customization/*/dist/*)
            return 0
            ;;
        *)
            return 1
            ;;
    esac
}

validate_path() {
    local relative_path="$1"
    if [[ -z "${relative_path}" || "${relative_path}" == /* || "${relative_path}" == *$'\n'* ]]; then
        printf 'Unsupported repository path: %q\n' "${relative_path}" >&2
        exit 1
    fi
}

add_copy_path() {
    local relative_path="$1"
    validate_path "${relative_path}"
    if ! is_generated_path "${relative_path}"; then
        copy_paths_set["${relative_path}"]=1
    fi
}

add_delete_path() {
    local relative_path="$1"
    validate_path "${relative_path}"
    if ! is_generated_path "${relative_path}"; then
        delete_paths_set["${relative_path}"]=1
    fi
}

collect_tracked_changes() {
    local -a diff_args=(
        --no-ext-diff
        --no-textconv
        --name-status
        -z
        --find-renames
        "${base_commit}"
    )
    local status old_path new_path

    if [[ "${source_mode}" == "head" ]]; then
        diff_args+=("${current_head}")
    fi
    diff_args+=(--)

    while IFS= read -r -d '' status; do
        case "${status}" in
            A*|M*|T*|U*|X*|B*)
                IFS= read -r -d '' new_path
                add_copy_path "${new_path}"
                ;;
            D*)
                IFS= read -r -d '' old_path
                add_delete_path "${old_path}"
                ;;
            R*)
                IFS= read -r -d '' old_path
                IFS= read -r -d '' new_path
                add_delete_path "${old_path}"
                add_copy_path "${new_path}"
                ;;
            C*)
                IFS= read -r -d '' old_path
                IFS= read -r -d '' new_path
                add_copy_path "${new_path}"
                ;;
            *)
                printf 'Unsupported git diff status: %s\n' "${status}" >&2
                exit 1
                ;;
        esac
    done < <(git -C "${repo_dir}" diff "${diff_args[@]}")
}

collect_tracked_changes

if [[ "${source_mode}" == "working-tree" ]]; then
    while IFS= read -r -d '' relative_path; do
        add_copy_path "${relative_path}"
    done < <(git -C "${repo_dir}" ls-files --others --exclude-standard -z)
fi

copy_paths=()
delete_paths=()
if (( ${#copy_paths_set[@]} )); then
    mapfile -t copy_paths < <(printf '%s\n' "${!copy_paths_set[@]}" | LC_ALL=C sort)
fi
if (( ${#delete_paths_set[@]} )); then
    mapfile -t delete_paths < <(printf '%s\n' "${!delete_paths_set[@]}" | LC_ALL=C sort)
fi

if [[ "${source_mode}" == "head" ]]; then
    if (( ${#copy_paths[@]} )); then
        git -C "${repo_dir}" archive --format=tar "${current_head}" -- "${copy_paths[@]}" \
            | tar -xf - -C "${stage_dir}/files"
    fi
else
    for relative_path in "${copy_paths[@]}"; do
        source_path="${repo_dir}/${relative_path}"
        destination_path="${stage_dir}/files/${relative_path}"
        if [[ ! -e "${source_path}" && ! -L "${source_path}" ]]; then
            printf 'Changed working-tree path disappeared while packaging: %s\n' "${relative_path}" >&2
            exit 1
        fi
        if [[ -d "${source_path}" && ! -L "${source_path}" ]]; then
            printf 'Changed gitlinks/directories are not supported in an overlay: %s\n' "${relative_path}" >&2
            exit 1
        fi
        mkdir -p "$(dirname "${destination_path}")"
        cp -a -- "${source_path}" "${destination_path}"
    done
fi

for relative_path in "${copy_paths[@]}"; do
    packaged_path="${stage_dir}/files/${relative_path}"
    if [[ ! -e "${packaged_path}" && ! -L "${packaged_path}" ]]; then
        printf 'Git archive did not produce a regular path (possibly a gitlink): %s\n' "${relative_path}" >&2
        exit 1
    fi
    if [[ -d "${packaged_path}" && ! -L "${packaged_path}" ]]; then
        printf 'Changed gitlinks/directories are not supported in an overlay: %s\n' "${relative_path}" >&2
        exit 1
    fi
done

if (( ${#delete_paths[@]} )); then
    printf '%s\n' "${delete_paths[@]}" > "${stage_dir}/DELETE_FILES.txt"
else
    : > "${stage_dir}/DELETE_FILES.txt"
fi

{
    printf 'SOURCE_MODE=%s\n' "${source_mode}"
    printf 'BASE_COMMIT=%s\n' "${base_commit}"
    printf 'HEAD_COMMIT=%s\n' "${current_head}"
    if [[ "${source_mode}" == "working-tree" ]]; then
        printf 'CONTENT=tracked-and-nonignored-untracked-working-tree-snapshot\n'
    else
        printf 'CONTENT=committed-head-snapshot\n'
    fi
} > "${stage_dir}/SOURCE.txt"

(
    cd "${stage_dir}/files"
    find . -type f -print0 | LC_ALL=C sort -z | xargs -0 -r sha256sum
) > "${stage_dir}/FILES.sha256"

find "${stage_dir}" -type d -exec chmod 0755 {} +
while IFS= read -r -d '' packaged_file; do
    if [[ -x "${packaged_file}" ]]; then
        chmod 0755 "${packaged_file}"
    else
        chmod 0644 "${packaged_file}"
    fi
done < <(find "${stage_dir}" -type f -print0)

source_epoch="$(git -C "${repo_dir}" show -s --format=%ct "${current_head}")"
find "${stage_dir}" -exec touch -h -d "@${source_epoch}" {} +

archive_path="${dist_dir}/${archive_name}"
if [[ -e "${archive_path}" ]]; then
    unlink "${archive_path}"
fi
(
    cd "${stage_parent}"
    find "${stage_name}" \( -type f -o -type l \) -print \
        | LC_ALL=C sort \
        | TZ=UTC zip -0 -X -D -y -q "${archive_path}" -@
)

printf '%s\n' "${archive_path}"
