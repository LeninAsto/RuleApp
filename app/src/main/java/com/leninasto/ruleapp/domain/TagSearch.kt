package com.leninasto.ruleapp.domain

data class TagInput(val prefix: String, val operator: String)
fun activeTagInput(query: String): TagInput {
    val token = query.substringAfterLast(' ')
    val operator = token.take(1).takeIf { it == "-" || it == "~" }.orEmpty()
    return TagInput(token.removePrefix(operator), operator)
}
fun completeTag(query: String, tag: String): String {
    val input = activeTagInput(query)
    val before = query.substringBeforeLast(' ', "")
    return (if (before.isEmpty()) "" else "$before ") + input.operator + tag + " "
}
/** Local suggestions reflect observed tags, never a promise that the server has a matching post. */
fun localTagSuggestions(source: Source, prefix: String, posts: List<BooruPost>, ratings: Set<Rating>): List<TagSuggestion> {
    if (prefix.isBlank() || ':' in prefix) return emptyList()
    return posts.asSequence().filter { it.key.siteId == source.name && it.rating in ratings }
        .distinctBy { it.key }.flatMap { it.tags.asSequence() }.filter { it.startsWith(prefix, ignoreCase = true) }
        .groupingBy { it }.eachCount().entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .take(12).map { TagSuggestion(it.key) }
}
