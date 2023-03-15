import os
import time
from googletrans import Translator
import requests
import re

# extracting all the input from environments
INPUT_PR_NUMBER = os.environ['INPUT_PR']

print("PR number: " + INPUT_PR_NUMBER)

printcache=""
def print2cache(str):
    global printcache
    printcache += str
    printcache += "\n"

url = 'https://github.com/fm-sys/snapdrop-android/pull/' + INPUT_PR_NUMBER + '.diff'
r = requests.get(url + "", allow_redirects=True)
text = r.content.decode()

translator = Translator()

table_initialized = False

for line in text.splitlines():
    if line.startswith("+"):
        match = re.search("<string.*?name=\"(.+?)\".*?>(.*)</string>", line)
        if match:
            if not table_initialized:
                table_initialized = True
                print2cache("ID|Translation|Reverse translated source string\n-|-|-")
            success = False
            while not success:
                try:
                    translation = translator.translate(match.group(2).replace("\\n", " \\n "))
                    success = True
                except:
                    print("API call blocked, wait some seconds and try again...")
                    time.sleep(10)
            print2cache(f"{match.group(1)}|{translation.origin} ({translation.src})|{translation.text} ({translation.dest})")
        elif line.startswith("+++"):
            print2cache("\n\n" + line + "\n")
            table_initialized = False


ACTION_ENV_DELIMITER = "__ENV_DELIMITER__"
def _build_file_input(name, value):
    return (
        f"{name}"
        f"<<{ACTION_ENV_DELIMITER}\n"
        f"{value}\n"
        f"{ACTION_ENV_DELIMITER}\n".encode("utf-8")
    )

with open(os.environ["GITHUB_OUTPUT"], "ab") as f:
    f.write(_build_file_input("content", printcache.strip()))
