
# How to replicate locally the same configuration that the integration loads at runtime

You can simply launch the following sh file that do the following:

```sh
TOKEN=kxbfgiertgibadsf

# starting the vault image exposing the port

docker run \
 --rm --detach --name vault -p 8200:8200 \
 -e "VAULT_DEV_ROOT_TOKEN_ID=$TOKEN" \
 -e 'SKIP_SETCAP=1' vault:1.6.3


# enabling the approle auth method

curl \
--header "X-Vault-Token: $TOKEN" \
--request POST \
--data '{"type": "approle" }' \
    http://127.0.0.1:8200/v1/sys/auth/approle

# creating a new policy to enable the approle to read/write from the test-secrets path

curl \
--request POST \
--header "X-Vault-Token: $TOKEN" \
--data '{"policy":"path \"secret/*\" { capabilities = [\"create\", \"read\", \"update\", \"delete\", \"list\"] }"}' \
http://127.0.0.1:8200/v1/sys/policy/test-secrets

# creating a new approle with the test-secrets permission

curl \
  --header "X-Vault-Token: $TOKEN" \
  --request POST \
  --data '{"policies": "test-secrets"}' \
  http://127.0.0.1:8200/v1/auth/approle/role/test-role

# getting the new role id

ROLE_ID=$(curl --header "X-Vault-Token: $TOKEN" http://127.0.0.1:8200/v1/auth/approle/role/test-role/role-id | jq -r '.data.role_id')

# getting the new secret id

SECRET_ID=$(curl --header "X-Vault-Token: $TOKEN" --request POST http://127.0.0.1:8200/v1/auth/approle/role/test-role/secret-id | jq -r '.data.secret_id')

# getting temp token after the login

ROLE_BASED_TEMP_TOKEN=$(curl --request POST --data "{\"role_id\":\"$ROLE_ID\",\"secret_id\":\"$SECRET_ID\"}" http://127.0.0.1:8200/v1/auth/approle/login | jq -r '.auth.client_token')

# inserting our awesome secret

curl \
    --header "X-Vault-Token: $ROLE_BASED_TEMP_TOKEN" \
    --request POST \
    --data '{ "options": { "cas": 0 }, "data": { "most-foundamental-question-of-the-universe": "the answer is 42" } }' \
    http://127.0.0.1:8200/v1/secret/data/my-secret

# getting back our awesome secret

curl \
    --header "X-Vault-Token: $ROLE_BASED_TEMP_TOKEN" \
    http://127.0.0.1:8200/v1/secret/data/my-secret
```
