import json
import requests
import time

with open('./question_bank.json') as jsonin:
    i = 0
    correct = 0
    for _ in jsonin:
        i += 1
        j = json.loads(_.strip())
        ans = j['^']
        headers = {'Content-Type': 'application/json'}
        while True:
            try:
                r = requests.post('http://localhost:8080', headers=headers, data=_.strip())
                if r.status_code == 200:
                    if ans == r.text:
                        correct += 1
                print "\rGot %d correct out of %d which is %.2f" % (correct, i, (float(correct) / i) * 100),
                break
            except:
                time.sleep(1)
                continue
