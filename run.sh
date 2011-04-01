git pull origin master
mvn -q versions:use-latest-releases
mvn -q versions:commit
mvn -q clean compile exec:java