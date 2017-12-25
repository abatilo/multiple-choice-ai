# multiple-choice-ai

Uses a trivial approach to using word2vec vectors for picking an answer to a
multiple choice question.

The implementation itself is all in
[QuestionGuesser](./service/src/main/java/QuestionGuesser.java) and the
intuition behind the approach can be found there as well.

Include with this project is an export of
[uberspot/OpenTriviaQA](https://github.com/uberspot/OpenTriviaQA) that has been
converted to JSON.  These questions are what the requests were modeled after.
The JSON form of this repo has been put at
[question_bank.json](./service/src/main/resources/question_bank.json)

The included [vectors.bin](./service/vectors.bin) was trained using the vanilla
word2vec [demo
script](https://github.com/abatilo/word2vec/blob/master/demo-word.sh) on the
text8 corpus.

## Getting Started

### Prerequisites

Requires Java 8 and Gradle 3.5 to be installed.

### Build Instructions
```
git clone https://github.com/abatilo/multiple-choice-ai.git
cd multiple-choice-ai
./gradlew :service:clean :service:shadowJar
java -jar service/multiple-choice-ai.jar server local.yaml
```

multiple-choice-ai is a Dropwizard service which will use the specified yaml
file to load in configurations.

### Usage
Once the server is running, a request can be made like so (assumes you have
[httpie](https://github.com/jakubroztocil/httpie) installed):

```
⇒  echo '{"#Q":"Bears are carnivores","A":"True","B": "False"}' | http POST :8080
HTTP/1.1 200 OK
Content-Length: 4
Content-Type: application/json
Date: Sun, 24 Dec 2017 08:31:23 GMT

True
```

Each request can have up to 4 possible answers to choose from:
```
⇒  echo '{"#Q":"All of these animals are omnivorous except one.","A":"Fox","B": "Mouse","C":"Opossum","D":"Snail"}' | http POST :8080
HTTP/1.1 200 OK
Content-Length: 5
Content-Type: application/json
Date: Sun, 24 Dec 2017 08:35:47 GMT

Snail
```

The response is nothing more than the text of the answer that the service
believes is correct.

If the request is malformed, you will receive an HTTP status of 400. If the
request was properly formed, but the service was unable to answer due to out of
vocabulary words, the service will return HTTP 422.

## Results

Unfortunately, the approach used only barely does better than random chance
when it comes to accuracy of the answers.
```
⇒  python evaluate.py
Got 14280 correct out of 48700 which is 29.32%
```

On the bright side, testing the service locally with
[siege](https://www.joedog.org/siege-home/) showed the approach to be pretty
fast.

```
⇒
siege -c40 -t30s --content-type "application/json" 'http://localhost:8080 POST {"#Q":"All of these animals are omnivorous except one.","A":"Fox","B":"Mouse","C":"Opossum","D":"Snail"}'
Transactions:                 162299 hits
Availability:                 100.00 %
Elapsed time:                  29.99 secs
Data transferred:               0.77 MB
Response time:                  0.01 secs
Transaction rate:            5411.77 trans/sec
Throughput:                     0.03 MB/sec
Concurrency:                   38.52
Successful transactions:      162301
Failed transactions:               0
Longest transaction:            0.08
Shortest transaction:           0.00
```

## Built With

* [Dropwizard](http://www.dropwizard.io/1.1.4/docs/) - The web framework used
* [Guava](https://github.com/google/guava/wiki/Release23) - Utility functions
* [OpenNLP](https://opennlp.apache.org/docs/1.8.2/manual/opennlp.html) - NLP library for doing text processing
* [Lombok](https://projectlombok.org/) - Annotations for less boilerplate code
* [word2vec](https://code.google.com/archive/p/word2vec/) - Included model was trained with the original C implementation of word2vec

## Contributing

Fork the project and submit a PR and one of the maintainers will be in touch.

## Authors

* Aaron Batilo - Developer / maintainer - [abatilo](https://github.com/abatilo)

See also the list of [contributors](https://github.com/abatilo/multiple-choice-ai/contributors) who participated in this project.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details
