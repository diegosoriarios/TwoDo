package com.diego.twodo

import com.diego.twodo.data.classifier.HeuristicClassifier
import com.diego.twodo.data.repository.ClassificationRepositoryImpl
import com.diego.twodo.domain.classifier.LlmClassifier
import com.diego.twodo.domain.model.BucketSource
import com.diego.twodo.domain.model.ClassificationResult
import com.diego.twodo.domain.model.Label
import com.diego.twodo.domain.model.Task
import com.diego.twodo.domain.model.TaskBucket
import com.diego.twodo.domain.repository.ClassificationScheduler
import com.diego.twodo.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReevaluationTest {

    @Test
    fun `single reevaluation overrides manual category`() = runBlocking {
        val task = task(id = 1, title = "call mom", bucket = TaskBucket.THIRTY_MIN, source = BucketSource.USER)
        val repository = FakeTaskRepository(listOf(task))
        val classification = classificationRepository(repository)

        val outcome = classification.reevaluateTask(task.id)

        assertEquals(TaskBucket.TWO_MIN, outcome?.newBucket)
        assertTrue(outcome?.changed == true)
        assertEquals(TaskBucket.TWO_MIN, repository.tasks.getValue(task.id).bucket)
        assertEquals(BucketSource.HEURISTIC, repository.tasks.getValue(task.id).bucketSource)
    }

    @Test
    fun `single reevaluation reports unchanged category`() = runBlocking {
        val task = task(id = 1, title = "call mom", bucket = TaskBucket.TWO_MIN)
        val repository = FakeTaskRepository(listOf(task))
        val classification = classificationRepository(repository)

        val outcome = classification.reevaluateTask(task.id)

        assertEquals(TaskBucket.TWO_MIN, outcome?.newBucket)
        assertFalse(outcome?.changed == true)
    }

    @Test
    fun `single reevaluation returns null when no category can be determined`() = runBlocking {
        val task = task(id = 1, title = "buy groceries and items", bucket = TaskBucket.THIRTY_MIN)
        val repository = FakeTaskRepository(listOf(task))
        val classification = classificationRepository(repository, llmResult = null)

        val outcome = classification.reevaluateTask(task.id)

        assertNull(outcome)
        assertEquals(TaskBucket.THIRTY_MIN, repository.tasks.getValue(task.id).bucket)
    }

    @Test
    fun `single reevaluation uses llm for ambiguous task`() = runBlocking {
        val task = task(id = 1, title = "buy groceries and items", bucket = TaskBucket.TWO_MIN)
        val repository = FakeTaskRepository(listOf(task))
        val llmResult = ClassificationResult(
            category = TaskBucket.THIRTY_MIN,
            reason = "Focused errand",
            source = BucketSource.LLM,
        )
        val classification = classificationRepository(repository, llmResult)

        val outcome = classification.reevaluateTask(task.id)

        assertEquals(TaskBucket.THIRTY_MIN, outcome?.newBucket)
        assertEquals(BucketSource.LLM, repository.tasks.getValue(task.id).bucketSource)
    }

    @Test
    fun `bulk reevaluation skips manual categories`() = runBlocking {
        val manual = task(id = 1, title = "call mom", bucket = TaskBucket.THIRTY_MIN, source = BucketSource.USER)
        val automatic = task(id = 2, title = "call dad", bucket = TaskBucket.THIRTY_MIN)
        val repository = FakeTaskRepository(listOf(manual, automatic))
        val classification = classificationRepository(repository)

        val summary = classification.reevaluateActiveTasks()

        assertEquals(1, summary.evaluated)
        assertEquals(1, summary.updated)
        assertEquals(1, summary.skipped)
        assertEquals(TaskBucket.THIRTY_MIN, repository.tasks.getValue(manual.id).bucket)
        assertEquals(BucketSource.USER, repository.tasks.getValue(manual.id).bucketSource)
        assertEquals(TaskBucket.TWO_MIN, repository.tasks.getValue(automatic.id).bucket)
    }

    @Test
    fun `bulk reevaluation can include manual categories`() = runBlocking {
        val manual = task(id = 1, title = "call mom", bucket = TaskBucket.THIRTY_MIN, source = BucketSource.USER)
        val repository = FakeTaskRepository(listOf(manual))
        val classification = classificationRepository(repository)

        val summary = classification.reevaluateActiveTasks(includeManual = true)

        assertEquals(1, summary.evaluated)
        assertEquals(1, summary.updated)
        assertEquals(0, summary.skipped)
        assertEquals(TaskBucket.TWO_MIN, repository.tasks.getValue(manual.id).bucket)
    }

    @Test
    fun `bulk reevaluation ignores completed tasks`() = runBlocking {
        val completed = task(
            id = 1,
            title = "call mom",
            bucket = TaskBucket.THIRTY_MIN,
            isCompleted = true,
        )
        val repository = FakeTaskRepository(listOf(completed))
        val classification = classificationRepository(repository)

        val summary = classification.reevaluateActiveTasks()

        assertEquals(0, summary.evaluated)
        assertEquals(0, summary.updated)
        assertEquals(0, summary.skipped)
        assertEquals(TaskBucket.THIRTY_MIN, repository.tasks.getValue(completed.id).bucket)
    }

    private fun classificationRepository(
        repository: TaskRepository,
        llmResult: ClassificationResult? = null,
    ) = ClassificationRepositoryImpl(
        taskRepository = repository,
        heuristicClassifier = HeuristicClassifier(),
        llmClassifier = FakeLlmClassifier(llmResult),
        classificationScheduler = FakeClassificationScheduler(),
    )

    private fun task(
        id: Long,
        title: String,
        bucket: TaskBucket,
        source: BucketSource = BucketSource.HEURISTIC,
        isCompleted: Boolean = false,
    ) = Task(
        id = id,
        title = title,
        bucket = bucket,
        bucketSource = source,
        isCompleted = isCompleted,
    )

    private class FakeLlmClassifier(
        private val result: ClassificationResult?,
    ) : LlmClassifier {
        override suspend fun classifyTask(taskTitle: String): ClassificationResult? = result

        override fun isModelLoaded(): Boolean = result != null
    }

    private class FakeClassificationScheduler : ClassificationScheduler {
        override fun schedule() = Unit
    }

    private class FakeTaskRepository(initialTasks: List<Task>) : TaskRepository {
        val tasks = initialTasks.associateBy { it.id }.toMutableMap()

        override suspend fun getTaskByIdSync(id: Long): Task? = tasks[id]

        override suspend fun getActiveTasksSync(): List<Task> =
            tasks.values.filter { !it.isCompleted }

        override suspend fun getUnclassifiedTasksSync(): List<Task> = emptyList()

        override suspend fun updateTaskBucket(
            id: Long,
            bucket: TaskBucket,
            source: BucketSource,
        ) {
            val task = tasks.getValue(id)
            tasks[id] = task.copy(bucket = bucket, bucketSource = source)
        }

        override fun getActiveTasks(): Flow<List<Task>> =
            flowOf(tasks.values.filter { !it.isCompleted })

        override suspend fun getCompletedSinceSync(since: Long): List<Task> = emptyList()

        override fun getCompletedSince(since: Long): Flow<List<Task>> = flowOf(emptyList())

        override fun getCompletedTasks(): Flow<List<Task>> = flowOf(emptyList())

        override fun getLabels(): Flow<List<Label>> = flowOf(emptyList())

        override fun getTaskLabelMap(): Flow<Map<Long, List<Label>>> = flowOf(emptyMap())

        override suspend fun createLabel(name: String): Long = 0

        override suspend fun setTaskLabels(taskId: Long, labelIds: Set<Long>) = Unit

        override fun getAllTasks(): Flow<List<Task>> = flowOf(tasks.values.toList())

        override fun getActiveTasksByBucket(bucket: TaskBucket): Flow<List<Task>> = flowOf(emptyList())

        override fun getUnclassifiedTasks(): Flow<List<Task>> = flowOf(emptyList())

        override fun getTaskById(id: Long): Flow<Task?> = flowOf(tasks[id])

        override suspend fun addTask(
            title: String,
            bucket: TaskBucket,
            source: BucketSource,
            dueDate: Long?,
            reminderAt: Long?,
        ): Long = error("Not used")

        override suspend fun updateTask(task: Task) {
            tasks[task.id] = task
        }

        override suspend fun updateTaskDetails(
            id: Long,
            title: String,
            bucket: TaskBucket?,
            dueDate: Long?,
            reminderAt: Long?,
            labelIds: Set<Long>,
        ) = Unit

        override suspend fun toggleTaskCompletion(id: Long, isCompleted: Boolean) = Unit

        override suspend fun deleteTask(task: Task) = Unit

        override suspend fun deleteTaskById(id: Long) = Unit
    }
}
