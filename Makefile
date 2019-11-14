default:
	javac -cp .:dependencies/commons-exec-1.3.jar:dependencies/commons-io-2.6.jar:dependencies/json-simple-2.1.2.jar:dependencies/org.json.jar -d . etbDL/engine/* etbDL/utils/* etbDL/etbDatalog.java etbDL/etbDatalogEngine.java etbDL/statements/* etbDL/output/* etbCS/utils/* etbCS/etbNode.java etbCS/clientMode.java etbCS/serverMode.java

all:
	javac -cp .:dependencies/commons-exec-1.3.jar:dependencies/commons-io-2.6.jar:dependencies/json-simple-2.1.2.jar:dependencies/org.json.jar -d . wrappers/* etbDL/engine/* etbDL/utils/* etbDL/etbDatalog.java etbDL/etbDatalogEngine.java etbDL/statements/* etbDL/output/* etbCS/utils/* etbCS/etbNode.java etbCS/clientMode.java etbCS/serverMode.java

clean:
	rm -rf etb
