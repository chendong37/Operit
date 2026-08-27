#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
bundle_dir="$(cd "${script_dir}/.." && pwd)"
dist_dir="${bundle_dir}/dist"
version="$(jq -r '.version' "${bundle_dir}/bundle.json")"
installation_guide_artifact="$(jq -r '.installationGuide' "${bundle_dir}/bundle.json")"
device_setup_guide_artifact="$(jq -r '.deviceSetupGuide' "${bundle_dir}/bundle.json")"
character_cards_artifact="$(jq -r '.characterCards' "${bundle_dir}/bundle.json")"
branding_master_artifact="$(jq -r '.brandingMaster' "${bundle_dir}/bundle.json")"
mapfile -t skill_artifacts < <(jq -r '.skills[]' "${bundle_dir}/bundle.json")

validate_artifact_path() {
    local artifact_path="$1"
    if [[ -z "${artifact_path}" || "${artifact_path}" == /* || \
        "${artifact_path}" == .. || "${artifact_path}" == ../* || \
        "${artifact_path}" == */../* || "${artifact_path}" == */.. || \
        "${artifact_path}" == *$'\n'* ]]; then
        printf 'Invalid bundle artifact path: %q\n' "${artifact_path}" >&2
        exit 1
    fi
}

artifacts=(
    "bundle.json"
    "${installation_guide_artifact}"
    "${device_setup_guide_artifact}"
    "${character_cards_artifact}"
    "${branding_master_artifact}"
    "${skill_artifacts[@]}"
)
for artifact_path in "${artifacts[@]}"; do
    validate_artifact_path "${artifact_path}"
done

stage_parent="$(mktemp -d)"
trap 'test -n "${stage_parent:-}" && test -d "${stage_parent}" && find "${stage_parent}" -depth -delete' EXIT
stage_dir="${stage_parent}/team-assistant-${version}"
mkdir -p "${stage_dir}" "${dist_dir}"

copy_bundle_artifact() {
    local source_path="$1"
    local artifact_path="$2"
    if [[ ! -f "${source_path}" || -L "${source_path}" ]]; then
        printf 'Bundle source must be a regular file: %s\n' "${source_path}" >&2
        exit 1
    fi
    mkdir -p "${stage_dir}/$(dirname "${artifact_path}")"
    cp -- "${source_path}" "${stage_dir}/${artifact_path}"
}

create_deterministic_zip() {
    local content_dir="$1"
    local output_path="$2"
    (
        cd "${content_dir}"
        find . -mindepth 1 -type f -print \
            | sed 's#^\./##' \
            | LC_ALL=C sort \
            | TZ=UTC zip -0 -X -D -q "${output_path}" -@
    )
}

for skill_artifact in "${skill_artifacts[@]}"; do
    skill_name="$(basename "${skill_artifact}" .zip)"
    skill_dir="${bundle_dir}/skills/${skill_name}"
    skill_stage_parent="${stage_parent}/skill-sources/${skill_name}"
    skill_stage_dir="${skill_stage_parent}/${skill_name}"
    if [[ ! -f "${skill_dir}/SKILL.md" || -L "${skill_dir}/SKILL.md" ]]; then
        printf 'Skill source must be a regular SKILL.md: %s\n' "${skill_dir}" >&2
        exit 1
    fi
    mkdir -p "${skill_stage_dir}" "${stage_dir}/$(dirname "${skill_artifact}")"
    cp -- "${skill_dir}/SKILL.md" "${skill_stage_dir}/SKILL.md"
    chmod 0644 "${skill_stage_dir}/SKILL.md"
    TZ=UTC touch -t 198001010000.00 "${skill_stage_dir}/SKILL.md"
    create_deterministic_zip \
        "${skill_stage_parent}" \
        "${stage_dir}/${skill_artifact}"
done

copy_bundle_artifact "${bundle_dir}/bundle.json" "bundle.json"
copy_bundle_artifact "${bundle_dir}/INSTALL.md" "${installation_guide_artifact}"
copy_bundle_artifact "${bundle_dir}/HYPEROS_SETUP.md" "${device_setup_guide_artifact}"
copy_bundle_artifact \
    "${bundle_dir}/character-cards/character_cards_backup.json" \
    "${character_cards_artifact}"
copy_bundle_artifact \
    "${bundle_dir}/${branding_master_artifact}" \
    "${branding_master_artifact}"

find "${stage_dir}" -type d -exec chmod 0755 {} +
find "${stage_dir}" -type f -exec chmod 0644 {} +
TZ=UTC find "${stage_dir}" -exec touch -t 198001010000.00 {} +

release_zip="${dist_dir}/team-assistant-${version}.zip"
staged_release_zip="${stage_parent}/team-assistant-${version}.zip"
create_deterministic_zip "${stage_dir}" "${staged_release_zip}"

for artifact_path in "${artifacts[@]}"; do
    mkdir -p "${dist_dir}/$(dirname "${artifact_path}")"
    cp -- "${stage_dir}/${artifact_path}" "${dist_dir}/${artifact_path}"
done
cp -- "${staged_release_zip}" "${release_zip}"

printf '%s\n' "${release_zip}"
