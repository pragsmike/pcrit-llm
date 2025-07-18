.PHONY: test

# Run the test suite for the library.
# This is the primary command to ensure the library is working correctly.
test:
	clj -X:test

# Create a 'pack' file containing all project files, suitable for
# uploading to an AI assistant for context.
pack:
	(for i in README.md deps.edn Makefile; \
	   do echo $$i; cat $$i; echo ----; done ; \
	 echo Source files; echo -----; \
	 (find src -name '*.clj' | xargs cat); \
	 echo Test files; echo -----; \
	 (find test -name '*.clj' | xargs cat) \
	) > ~/pcrit-llm-pack.txt
