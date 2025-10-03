# SFTP Outbox

**This repo contains extracted dir `sda-sftp-inbox` from the https://github.com/neicnordic/sensitive-data-archive/tree/main/sda-sftp-inbox repo and modified as an outbox component.**

## Federated EGA/LocalEGA login system

`CentralEGA` contains a database of users, with IDs and passwords.

A solution has been devised using [Apache Mina SSHD](https://mina.apache.org/sshd-project/) to facilitate user authentication through either a password or an RSA key, directly against the CentralEGA database. The user is locked within their home folder, which is done programmatically using 
[RootedFileSystem](https://github.com/apache/mina-sshd/blob/master/sshd-core/src/main/java/org/apache/sshd/common/file/root/RootedFileSystem.java).

The solution uses `CentralEGA`'s user IDs but can also be extended to
use LifeScience AAI IDs (from which the ``@elixir-europe.org`` suffix is removed).

The procedure is as follows. The outbox is started without any created
user. When a user wants to log into the outbox (actually, only ``sftp``
uploads are allowed), the code looks up the username in a local
cache, and, if not found, queries the `CentralEGA` [REST endpoint](https://nss.ega-archive.org/spec/). Upon the user's return, their credentials are stored in the local cache, and a home directory is established for the user. The user now gets logged in if the password
or public key authentication succeeds. Upon subsequent login attempts,
only the local cache is queried, until the user's credentials
expire. The cache has a default TTL of 5 minutes, and is wiped clean
upon reboot (as a cache should). Default TTL can be configured via ``CACHE_TTL`` env var.

The user's home directory is created when its credentials upon successful login.
Moreover, for each user, detection is performed to ascertain when the file upload is completed, and the checksum for the uploaded file is computed.

## S3 integration

Default storage back-end for the outbox is local file-system. Additionally, support for the S3 service is provided as a back-end option. It can be 
enabled using S3-related env-vars (see configuration details below).

If S3 is enabled, then files are still going to be stored locally, but after successful upload, they will going to be 
uploaded to the specified S3 back-end. With this approach local file-system plays role of so called "staging area", 
while S3 is the real final destination for the uploaded files.

## Configuration

Environment variables used:


| Variable name       | Default value       | Description                                                    |
|---------------------|---------------------|----------------------------------------------------------------|
| OUTBOX_PORT         | 2222                | Outbox port                                                    |
| OUTBOX_LOCATION     | /ega/outbox/        | Path to POSIX Outbox backend                                   |
| OUTBOX_FS_PATH      |                     | Prefix path when custom filesystem is used on top of POSIX     |
| OUTBOX_KEYPAIR      |                     | Path to RSA keypair file                                       |
| KEYSTORE_TYPE       | JKS                 | Keystore type to use, JKS or PKCS12                            |
| KEYSTORE_PATH       | /etc/ega/outbox.jks | Path to Keystore file                                          |
| KEYSTORE_PASSWORD   |                     | Password to access the Keystore                                |
| CACHE_TTL           | 300.0               | CEGA credentials time-to-live                                  |
| CEGA_ENDPOINT       |                     | CEGA REST endpoint                                             |
| CEGA_ENDPOINT_CREDS |                     | CEGA REST credentials                                          |
| S3_ENDPOINT         | outbox-backend:9000 | Outbox S3 backend URL                                          |
| S3_REGION           | us-east-1           | Outbox S3 backend region (us-east-1 is default in Minio)       |
| S3_ACCESS_KEY       |                     | Outbox S3 backend access key (S3 disabled if not specified)    |
| S3_SECRET_KEY       |                     | Outbox S3 backend secret key (S3 disabled if not specified)    |
| S3_BUCKET           |                     | Outbox S3 backend secret bucket (S3 disabled if not specified) |
| USE_SSL             | true                | true if S3 Outbox backend should be accessed by HTTPS          |
| LOGSTASH_HOST       |                     | Hostname of the Logstash instance (if any)                     |
| LOGSTASH_PORT       |                     | Port of the Logstash instance (if any)                         |

If `LOGSTASH_HOST` or `LOGSTASH_PORT` is empty, Logstash logging will not be enabled.

In addition, environment variables can be used to configure log level for different packages. Package loggers can be configured using corresponding package names, for example, to turn of logs of Spring, one can set environment variable `LOGGING_LEVEL_ORG_SPRINGFRAMEWORK=OFF`, or to set Mina's own logs to debug: `LOGGING_LEVEL_SE_NBIS_LEGA_OUTBOX=DEBUG`, etc.

### SFTP Outbox Local Development/Testing

For local development/testing see instructions in [dev_utils](https://github.com/neicnordic/sensitive-data-archive/tree/main/sda-sftp-outbox/dev_utils) folder.
There is an README file in the [dev_utils](https://github.com/neicnordic/sensitive-data-archive/tree/main/sda-sftp-outbox/dev_utils) folder with sections for running the pipeline locally using Docker Compose.