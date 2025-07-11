#!/usr/bin/env python3
#
# Copyright (C) 2025 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


import argparse
import requests
import json

def create_confirmation_request(base_url, dve_path, storage_root, nbn, version):
    url = f"{base_url}/confirmationRequests"
    payload = {
        "dvePath": dve_path,
        "storageRoot": storage_root,
        "nbn": nbn,
        "version": version
    }
    headers = {
        "Content-Type": "application/json"
    }

    try:
        response = requests.post(url, json=payload, headers=headers)
        if response.status_code == 201:
            print("Confirmation request created successfully.")
        elif response.status_code == 409:
            print("Confirmation request already exists.")
        else:
            print(f"Failed to create confirmation request. Status code: {response.status_code}")
            print(f"Response: {response.text}")
    except requests.exceptions.RequestException as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Create a confirmation request. This script is intended for development use only.")
    parser.add_argument("--base-url", default='http://localhost:20370', help="Base URL of the API (default: %(default)s)")
    parser.add_argument("--dve-path", required=True, help="Absolute path to the DVE to be confirmed")
    parser.add_argument("--storage-root", required=True, help="Shortname for the OCFL Storage Root")
    parser.add_argument("--nbn", required=True, help="NBN identifier")
    parser.add_argument("--version", type=int, required=True, help="Version number")

    args = parser.parse_args()

    create_confirmation_request(
        base_url=args.base_url,
        dve_path=args.dve_path,
        storage_root=args.storage_root,
        nbn=args.nbn,
        version=args.version
    )