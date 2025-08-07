#!/bin/sh
# Maven Wrapper 3.9.11

MAVEN_VERSION=3.9.11
MAVEN_PROJECTBASEDIR="$PWD"
MAVEN_HOME="$HOME/.m2/wrapper/dists/apache-maven-$MAVEN_VERSION"

# OS specific support
cygwin=false
darwin=false
mingw=false
case "$(uname)" in
  CYGWIN*) cygwin=true ;;
  MINGW*) mingw=true;;
  Darwin*) darwin=true ;;
esac

# Maven executable
if [ -z "$JAVA_HOME" ]; then
  javaExecutable="java"
else
  javaExecutable="$JAVA_HOME/bin/java"
fi

exec "$javaExecutable" \
  -classpath "$MAVEN_HOME/lib/*" \
  "-Dmaven.home=$MAVEN_HOME" \
  "-Dmaven.multiModuleProjectDirectory=$MAVEN_PROJECTBASEDIR" \
  org.apache.maven.cli.MavenCli "$@"