package com.epam.brn.upload.csv.processor

import com.epam.brn.constant.ExerciseTypeEnum
import com.epam.brn.constant.WordTypeEnum
import com.epam.brn.constant.mapPositionToWordType
import com.epam.brn.exception.EntityNotFoundException
import com.epam.brn.model.Exercise
import com.epam.brn.model.Resource
import com.epam.brn.model.Task
import com.epam.brn.repo.ExerciseRepository
import com.epam.brn.service.ExerciseService
import com.epam.brn.service.ResourceService
import com.epam.brn.service.SeriesService
import org.apache.commons.lang3.StringUtils
import org.apache.logging.log4j.kotlin.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class SeriesTwoExerciseRecordProcessor(
    private val resourceService: ResourceService,
    private val seriesService: SeriesService,
    private val exerciseService: ExerciseService,
    private val exerciseRepository: ExerciseRepository
) {
    private val log = logger()

    val EXERCISE_NAME = "exerciseName"
    val LEVEL = "level"
    val WORDS = "words"

    @Value(value = "\${brn.audio.file.second.series.path}")
    private lateinit var audioFileUrl: String

    @Value(value = "\${brn.picture.file.default.path}")
    private lateinit var pictureFileUrl: String

    fun process(exercises: MutableList<Map<String, Any>>): List<Exercise> {
        val result = exercises.map { parsedValue -> convert(parsedValue) }
        return exerciseRepository.saveAll(result)
    }

    private fun convert(source: Map<String, Any>): Exercise {
        val target = createOrGetExercise(source)
        convertExercise(source, target)
        convertTask(target)
        convertResources(source, target)

        return target
    }

    private fun createOrGetExercise(source: Map<String, Any>): Exercise {
        val name = source[EXERCISE_NAME].toString()
        val level = source[LEVEL].toString().toInt()

        return try {
            val exercise = exerciseService.findExerciseByNameAndLevel(name, level)

            log.debug("exercise with name {$name} and level {$level} is already persisted. Entity Will be updated")

            exercise
        } catch (e: EntityNotFoundException) {
            Exercise(name = name, level = level)
        }
    }

    private fun convertExercise(source: Map<String, Any>, target: Exercise) {
        target.description = source[EXERCISE_NAME].toString()
        target.exerciseType = ExerciseTypeEnum.WORDS_SEQUENCES.toString()
        target.series = seriesService.findSeriesForId(2L)
    }

    private fun convertTask(target: Exercise) {
        val task = Task()
        task.serialNumber = 2
        task.exercise = target
        target.tasks.add(task)
    }

    private fun convertResources(source: Map<String, Any>, target: Exercise) {
        val templateList = arrayListOf<String>()

        source[WORDS]
            .toString()
            .split(";")
            .mapIndexed { index, element ->
                val wordType = mapPositionToWordType[index]
                val resources = createOrGetResources(element, wordType)

                if (resources.isNotEmpty()) {
                    templateList.add(wordType.toString())
                }

                resources
            }
            .map { resource -> target.tasks.first().answerOptions.addAll(resource) }

        target.template = templateList.joinToString(StringUtils.SPACE, "<", ">")
    }

    private fun createOrGetResources(words: String, wordType: WordTypeEnum?): List<Resource> {
        return words.split(StringUtils.SPACE)
            .asSequence()
            .map { word -> word.replace("[()]".toRegex(), StringUtils.EMPTY) }
            .filter { word -> StringUtils.isNotEmpty(word) }
            .map { word -> getResourceByWord(word) }
            .map { resource -> setWordType(resource, wordType) }
            .map(resourceService::save)
            .toList()
    }

    private fun getResourceByWord(word: String): Resource {
        return resourceService.findFirstResourceByWordLike(word)
            ?: createAndGetResource(word, WordTypeEnum.UNKNOWN.toString())
    }

    private fun createAndGetResource(word: String, wordType: String): Resource {
        val resource = Resource()
        resource.word = word
        resource.wordType = WordTypeEnum.valueOf(wordType).toString()
        resource.audioFileUrl = audioFileUrl.format(word)
        resource.pictureFileUrl = pictureFileUrl.format(word)

        return resource
    }

    private fun setWordType(resource: Resource, wordType: WordTypeEnum?): Resource {
        resource.wordType = wordType?.toString() ?: WordTypeEnum.UNKNOWN.toString()
        log.debug("Word type for resource with id {${resource.id}} was updated to {${resource.wordType}}")
        return resource
    }
}