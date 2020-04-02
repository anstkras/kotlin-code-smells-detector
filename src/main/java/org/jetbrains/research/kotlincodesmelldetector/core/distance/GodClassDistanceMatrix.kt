package org.jetbrains.research.kotlincodesmelldetector.core.distance

import com.intellij.openapi.progress.ProgressIndicator
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.research.kotlincodesmelldetector.utils.functions
import org.jetbrains.research.kotlincodesmelldetector.utils.generateFullEntitySets
import org.jetbrains.research.kotlincodesmelldetector.utils.math.Clustering
import org.jetbrains.research.kotlincodesmelldetector.utils.properties
import org.jetbrains.research.kotlincodesmelldetector.utils.toPointer

private const val maximumNumberOfSourceClassMembersAccessedByExtractClassCandidate = 2

fun getJaccardDistanceMatrix(entities: List<KtElement>): Array<DoubleArray> {
    val jaccardDistanceMatrix =
        Array(entities.size) { DoubleArray(entities.size) }
    val entitySets = generateFullEntitySets(entities)
    for (i in jaccardDistanceMatrix.indices) {
        for (j in jaccardDistanceMatrix.indices) {
            if (i != j) {
                jaccardDistanceMatrix[i][j] = DistanceCalculator.getDistance(
                    entitySets[entities[i]] ?: error("No entity set was generated for ${entities[i]}"),
                    entitySets[entities[j]] ?: error("No entity set was generated for ${entities[j]}")
                )
            } else {
                jaccardDistanceMatrix[i][j] = 0.0
            }
        }
    }

    return jaccardDistanceMatrix
}

fun getExtractClassCandidateRefactorings(
    projectInfo: ProjectInfo,
    classesToBeExamined: List<KtClassOrObject>,
    indicator: ProgressIndicator
): List<ExtractClassCandidateRefactoring> {
    val candidateList: MutableList<ExtractClassCandidateRefactoring> = mutableListOf()

    indicator.text = "Identification of Extract Class refactoring opportunities"
    indicator.fraction = 0.0
    for (sourceClass in classesToBeExamined) {
        if (classIsBigEnough(sourceClass)) {
            val entities = mutableListOf<KtElement>()
            entities.addAll(sourceClass.properties)
            entities.addAll(sourceClass.functions)

            val distanceMatrix: Array<DoubleArray> = getJaccardDistanceMatrix(entities)
            val clustering = Clustering.getInstance(0, distanceMatrix)
            val clusters = clustering.clustering(entities)
            var processedClusters = 0
            for (cluster in clusters) {
                processedClusters += 1
                indicator.fraction = processedClusters.toDouble() / clusters.size
                val candidate =
                    ExtractClassCandidateRefactoring(projectInfo, sourceClass.toPointer(), cluster.entities)
                if (candidate.isApplicable) {
                    val sourceClassDependencies = candidate.distinctSourceDependencies
                    val extractedClassDependencies = candidate.distinctTargetDependencies
                    if (sourceClassDependencies <= maximumNumberOfSourceClassMembersAccessedByExtractClassCandidate &&
                        sourceClassDependencies < extractedClassDependencies
                    ) {
                        candidateList.add(candidate)
                    }
                }
            }
            // Clustering End
        }
    }

    indicator.fraction = 1.0
    return candidateList
}

private fun classIsBigEnough(sourceClass: KtClassOrObject?): Boolean {
    //TODO
    return true
}