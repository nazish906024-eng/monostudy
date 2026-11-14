package com.example.data.model

import kotlinx.serialization.Serializable

@Serializable
data class FrequencyEstimate(
    val marker: String = "Medium",
    val estimated: Boolean = true
)

@Serializable
data class Chapter(
    val id: String,
    @kotlinx.serialization.SerialName("class")
    val cls: Int,
    val subject: String,
    val title: String,
    val weightage: String = "Medium",
    val past10YearQuestionFrequency: FrequencyEstimate = FrequencyEstimate()
)

@Serializable
data class ChapterCatalog(
    val schemaVersion: Int = 1,
    val notice: String = "",
    val chapters: List<Chapter> = emptyList()
)

@Serializable
data class PyqQuestion(
    val id: String,
    val exam: String = "JEE",
    val year: Int? = null,
    val subject: String = "Physics",
    val chapterId: String = "",
    val difficulty: String = "Medium",
    val tags: List<String> = emptyList(),
    val q: String = "",
    val options: List<String> = emptyList(),
    val answer: Int = 0,
    val explain: String = "",
    val custom: Boolean = false
)

@Serializable
data class PyqCatalog(
    val notice: String = "",
    val questions: List<PyqQuestion> = emptyList()
)

data class Flashcard(
    val id: String,
    val subject: String,
    val front: String,
    val back: String
)

object BundledFlashcards {
    val deck: List<Flashcard> = listOf(
        Flashcard("f1", "Physics", "Equations of motion", "v = u + at,  s = ut + ½at²,  v² = u² + 2as"),
        Flashcard("f2", "Physics", "Projectile range", "R = (u² sin 2θ) / g, maximum at θ = 45°"),
        Flashcard("f3", "Physics", "Work–energy theorem", "W_net = ΔK = ½mv² − ½mu²"),
        Flashcard("f4", "Physics", "Resistors in parallel", "1 / R_eq = 1 / R₁ + 1 / R₂ + …"),
        Flashcard("f5", "Physics", "Faraday’s law", "ε = −dΦ_B / dt"),
        Flashcard("f6", "Chemistry", "First-order half-life", "t₁/₂ = 0.693 / k;  k has unit s⁻¹"),
        Flashcard("f7", "Chemistry", "Nernst equation (298 K)", "E = E° − (0.0591 / n) log Q"),
        Flashcard("f8", "Chemistry", "Ideal gas law", "PV = nRT"),
        Flashcard("f9", "Mathematics", "Quadratic formula", "x = (−b ± √(b² − 4ac)) / 2a;  sum of roots = −b/a"),
        Flashcard("f10", "Mathematics", "Euler’s formula", "e^(iθ) = cos θ + i sin θ"),
        Flashcard("f11", "Mathematics", "Powers of i", "i² = −1,  i³ = −i,  i⁴ = 1  (cycle of 4)"),
        Flashcard("f12", "Biology", "Five-kingdom system", "Monera, Protista, Fungi, Plantae, Animalia — Monera holds all prokaryotes."),
        Flashcard("f13", "Biology", "Site of fertilisation (human)", "Ampullary region of the fallopian tube."),
        Flashcard("f14", "Biology", "Double fertilisation", "One male gamete + egg → zygote; the other + two polar nuclei → triploid primary endosperm nucleus.")
    )
}
