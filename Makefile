run: jar
	java -jar out/libs/query-builder.jar

jlink: jar
	jlink --module-path out/libs/ --add-modules querybuilder --output out/standalone

jar: compile out/.jar-stamp

out/.jar-stamp: $(shell find out/classes -name '*.class')
	jar --create \
		--file out/libs/query-builder.jar \
		--main-class querybuilder.Main \
		--module-version 1.0 \
		-C out/classes/querybuilder .
	touch $@

compile: out/.compile-stamp

out/.compile-stamp: $(shell find src/main/java -name '*.java')
	javac -d out/classes --module-source-path src/main/java/ --module querybuilder
	touch $@

clean:
	rm -rf out/

