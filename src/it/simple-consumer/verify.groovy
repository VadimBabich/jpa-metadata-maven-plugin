import java.nio.file.Files
import java.nio.file.Path

// 1. Generated sources compiled — the whole point of this IT.
def compiledClasses = [
    'target/classes/com/example/model/User_.class',
    'target/classes/com/example/model/UserAttribute_.class',
    'target/classes/org/springframework/data/relational/core/sql/Column_.class',
    'target/classes/org/springframework/data/r2dbc/config/StaticR2dbcEntityTemplateAccessor_.class',
]
compiledClasses.each { relative ->
    def file = new File(basedir, relative)
    assert file.isFile() : "Generated source did not compile: ${relative} is missing"
}

// 2. Generated sources match the committed golden corpus byte-for-byte (shape freeze:
//    the future annotation processor must reproduce these files exactly — decision D1).
Path expectedRoot = new File(basedir, 'expected').toPath()
Path actualRoot = new File(basedir, 'target/generated-sources/metamodel').toPath()

assert Files.isDirectory(expectedRoot) : 'expected/ golden corpus directory is missing'
assert Files.isDirectory(actualRoot) : 'generated-sources/metamodel was not produced'

def relativeJavaFiles = { Path root ->
    def result = [] as SortedSet
    root.toFile().eachFileRecurse { f ->
        if (f.isFile() && f.name.endsWith('.java')) {
            result << root.relativize(f.toPath()).toString()
        }
    }
    result
}

def expectedFiles = relativeJavaFiles(expectedRoot)
def actualFiles = relativeJavaFiles(actualRoot)

assert actualFiles == expectedFiles :
    "Generated file set differs from golden corpus.\nExpected: ${expectedFiles}\nActual:   ${actualFiles}"

expectedFiles.each { relative ->
    byte[] expected = Files.readAllBytes(expectedRoot.resolve(relative))
    byte[] actual = Files.readAllBytes(actualRoot.resolve(relative))
    assert Arrays.equals(expected, actual) :
        "Generated file differs from golden corpus: ${relative}"
}

println "Verified: generated sources compile and match the golden corpus (${expectedFiles.size()} files)."
return true
