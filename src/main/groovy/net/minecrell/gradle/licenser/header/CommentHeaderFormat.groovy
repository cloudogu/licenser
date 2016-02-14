package net.minecrell.gradle.licenser.header

import net.minecrell.gradle.licenser.util.HeaderHelper

class CommentHeaderFormat implements HeaderFormat {

    final String name

    final String start
    final String end

    final String firstLine
    final String prefix
    final String lastLine

    CommentHeaderFormat(String name, String start, String end, String firstLine, String prefix, String lastLine) {
        this.name = name
        this.start = start
        this.end = end
        this.firstLine = firstLine
        this.prefix = prefix
        this.lastLine = lastLine
    }

    protected List<String> format(String text) {
        ensureAbsent(text, firstLine)
        ensureAbsent(text, lastLine)

        List<String> result = [firstLine]

        text.eachLine {
            result << HeaderHelper.stripTrailingIndent("$prefix $it")
        }

        result << lastLine

        return result
    }

    private static void ensureAbsent(String s, String search) {
        if (s.contains(search)) {
            throw new IllegalArgumentException("Header contains unsupported characters $search")
        }
    }

    @Override
    PreparedHeader prepare(String text) {
        return new PreparedCommentHeader(this, format(text))
    }

}