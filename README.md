# multiple-choice-ai

Uses a trivial approach to using word2vec vectors for picking an answer to a
multiple choice question.

## Getting Started

```
git clone https://github.com/abatilo/multiple-choice-ai.git
cd multiple-choice-ai
./gradlew :service:clean :service:shadowJar
java -jar service/multiple-choice-ai.jar server local.yaml
```

multiple-choice-ai is a Dropwizard service which will use the specified yaml
fileto load in configurations.

### Prerequisites

Requires Java 8 to be installed.

## Built With

* [Dropwizard](http://www.dropwizard.io/1.1.4/docs/) - The web framework used
* [Guava](https://github.com/google/guava/wiki/Release23) - Utility functions
* [OpenNLP](https://opennlp.apache.org/docs/1.8.2/manual/opennlp.html) - NLP library for doing text processing
* [Lombok](https://projectlombok.org/) - Annotations for less boilerplate code

## Contributing

Fork the project and submit a PR and one of the maintainers will be in touch.

## Authors

* Aaron Batilo - Developer / maintainer - [abatilo](https://github.com/abatilo)

See also the list of [contributors](https://github.com/your/project/contributors) who participated in this project.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details
