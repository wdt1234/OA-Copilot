$MAVEN_PROJECTBASEDIR = Split-Path -Parent $MyInvocation.MyCommand.Path
$WRAPPER_JAR = Join-Path $MAVEN_PROJECTBASEDIR ".mvn\wrapper\maven-wrapper.jar"

$env:MAVEN_OPTS = "-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -Dconsole.encoding=UTF-8"

java "-Dmaven.multiModuleProjectDirectory=$MAVEN_PROJECTBASEDIR" -cp $WRAPPER_JAR org.apache.maven.wrapper.MavenWrapperMain @args
