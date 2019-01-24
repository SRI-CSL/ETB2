default:
	javac -cp .:dependencies/commons-exec-1.3.jar:dependencies/commons-io-2.6.jar:dependencies/json-simple-2.1.2.jar:dependencies/org.json.jar -d . etbDL/engine/* etbDL/utils/* etbDL/etbDatalog.java etbDL/etbDatalogEngine.java etbDL/statement/* etbDL/output/* etbDL/services/* etbCS/utils/* etbCS/etbNode.java etbCS/clientMode.java

all:
	javac -cp .:dependencies/commons-exec-1.3.jar:dependencies/commons-io-2.6.jar:dependencies/json-simple-2.1.2.jar:dependencies/org.json.jar -d . wrappers/* etbDL/engine/* etbDL/utils/* etbDL/etbDatalog.java etbDL/etbDatalogEngine.java etbDL/statement/* etbDL/output/* etbDL/services/* etbCS/utils/* etbCS/etbNode.java etbCS/clientMode.java

clean:
	rm -rf etb
