import requests
import os
import json


def extract_modified_files(event_path):
    with open(os.environ['GITHUB_EVENT_PATH']) as file:
        event_data = json.load(file)
    
    ##  Pull request events have a different structure than push events.
    if 'pull_request' in event_data:
        url = event_data['pull_request']['diff_url']
    else:
        url = event_data['compare']
    
    return url

def send_slack_notification(webhook_url, repository, url):
    message = f"One or more monitored files has changed in repository `{repository}`.\n\nThe changed files can be seen here: {url}"
    payload = {
        "text": message,
    }
    requests.post(webhook_url, json=payload)

if __name__ == "__main__":
    webhook_url = os.environ.get("SLACK_WEBHOOK_URL")
    repository = os.environ.get("GITHUB_REPOSITORY")
    filename = os.environ.get("GITHUB_EVENT_PATH")
    
    url = extract_modified_files(filename)

    if webhook_url and repository and filename:
        send_slack_notification(webhook_url, repository, url)
    else:
        print("Missing environment variables. Unable to send notification.")
