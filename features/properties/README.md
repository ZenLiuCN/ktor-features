# ktor-features
# properties feature
## feature

properties manager for ktor

with singleton `PropertiesManager` could extract `application.conf` section into `class` **!! NOT `data class`**

## example
```kotlin
@Properties("Some")
data class SomeConf(
    var name:String=""
)

//...
val conf=PropertiesManager.properties<SomeConf>()!!
//get SomeConf(name="abc")

```
```hocon
//...
Some{
    name:abc
}
//...
```
## notes

**CASE-Sensitive** : properties is case-sensitive

**NULL** : when some none default value property not found will throw exception and return `NULL` value

**CLASS**: must be `class` not `data class`
## dependencies
```groovy
dependencies {
        compileOnly "io.github.config4k:config4k:$config4k_version"
        compileOnly "io.ktor:ktor-server-core:$ktor_version"
}

```
