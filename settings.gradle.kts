rootProject.name = "CustomBlockGroups"

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

include(":RoosterUI")
project(":RoosterUI").projectDir = file("../RoosterUI")

include(":RoosterCommon")
project(":RoosterCommon").projectDir = file("../RoosterUI/RoosterCommon")

include(":RoosterLocalization")
project(":RoosterLocalization").projectDir = file("../RoosterUI/RoosterLocalization")

include(":RoosterSql")
project(":RoosterSql").projectDir = file("../RoosterSql")
