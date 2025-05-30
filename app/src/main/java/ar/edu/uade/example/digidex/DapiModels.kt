package ar.edu.uade.example.digidex

data class DapiDigimonResponse(
    val id: Int,
    val name: String,
    val images: List<Image> = emptyList(),
    val levels: List<Level> = emptyList(),
    val attributes: List<Attribute> = emptyList(),
    val types: List<Type> = emptyList(),
    val fields: List<Field> = emptyList(),
    val releaseDate: String? = null,
    val descriptions: List<Description> = emptyList()
)

data class Type(val type: String)
data class Field(val field: String)
data class Description(val origin: String, val language: String, val description: String)
data class Image(val href: String)
data class Level(val level: String)
data class Attribute(val attribute: String)

data class DapiDigimonSummary(
    val id: Int,
    val name: String
)

data class DapiDigimonListResponse(
    val content: List<DapiDigimonSummary>,
    val pageable: Pageable
)

data class Pageable(
    val currentPage: Int,
    val elementsOnPage: Int,
    val totalElements: Int,
    val totalPages: Int,
    val previousPage: String?,
    val nextPage: String?
)
