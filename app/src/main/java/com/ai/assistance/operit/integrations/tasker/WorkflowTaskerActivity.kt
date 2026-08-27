package com.ai.assistance.operit.integrations.tasker

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.ai.assistance.operit.R
import com.ai.assistance.operit.data.repository.WorkflowRepository
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerAction
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess
import kotlinx.coroutines.runBlocking

/** Tasker configuration screen for a workflow command. */
class WorkflowTaskerActivityConfig : Activity(), TaskerPluginConfig<WorkflowTaskerInput> {

    override val context: Context get() = applicationContext

    private val taskerHelper by lazy { WorkflowTaskerConfigHelper(this) }
    private lateinit var commandInput: EditText

    override val inputForTasker: TaskerInput<WorkflowTaskerInput>
        get() = TaskerInput(
            WorkflowTaskerInput(command = commandInput.text?.toString()?.trim().orEmpty())
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val horizontalPadding = (24 * resources.displayMetrics.density).toInt()
        val verticalPadding = (16 * resources.displayMetrics.density).toInt()
        commandInput = EditText(this).apply {
            hint = getString(R.string.workflow_tasker_command_hint)
            inputType = InputType.TYPE_CLASS_TEXT
            isSingleLine = true
        }
        val saveButton = Button(this).apply {
            text = getString(R.string.workflow_tasker_save)
            setOnClickListener {
                if (commandInput.text?.toString()?.trim().isNullOrEmpty()) {
                    commandInput.error = getString(R.string.workflow_tasker_command_required)
                } else {
                    taskerHelper.finishForTasker()
                }
            }
        }
        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
                addView(
                    TextView(this@WorkflowTaskerActivityConfig).apply {
                        text = getString(R.string.workflow_tasker_command_desc)
                    },
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ),
                )
                addView(
                    commandInput,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ),
                )
                addView(
                    saveButton,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ),
                )
            },
        )
        taskerHelper.onCreate()
    }

    override fun assignFromInput(input: TaskerInput<WorkflowTaskerInput>) {
        commandInput.setText(input.regular.command.orEmpty())
    }
}

class WorkflowTaskerConfigHelper(config: TaskerPluginConfig<WorkflowTaskerInput>) :
    TaskerPluginConfigHelper<WorkflowTaskerInput, Unit, WorkflowTaskerRunner>(config) {

    override val inputClass: Class<WorkflowTaskerInput> = WorkflowTaskerInput::class.java
    override val outputClass: Class<Unit> = Unit::class.java
    override val runnerClass: Class<WorkflowTaskerRunner> = WorkflowTaskerRunner::class.java

    override fun addToStringBlurb(
        input: TaskerInput<WorkflowTaskerInput>,
        blurbBuilder: StringBuilder,
    ) {
        blurbBuilder.append("Zhixing AI workflow: ")
        blurbBuilder.append(input.regular.command.orEmpty())
    }
}

@TaskerInputRoot
class WorkflowTaskerInput @JvmOverloads constructor(
    @field:TaskerInputField("command") var command: String? = null,
)

class WorkflowTaskerRunner : TaskerPluginRunnerAction<WorkflowTaskerInput, Unit>() {

    override fun run(
        context: Context,
        input: TaskerInput<WorkflowTaskerInput>,
    ): TaskerPluginResult<Unit> {
        val command = input.regular.command?.trim().orEmpty()
        if (command.isEmpty()) {
            return TaskerPluginResultError(
                IllegalArgumentException(context.getString(R.string.workflow_tasker_command_required))
            )
        }

        return try {
            runBlocking {
                WorkflowRepository(context.applicationContext)
                    .triggerWorkflowsByTaskerEvent(listOf(command))
            }
            TaskerPluginResultSucess()
        } catch (e: Exception) {
            TaskerPluginResultError(e)
        }
    }
}
