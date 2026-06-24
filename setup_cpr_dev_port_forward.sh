#!/bin/bash -e

# ❗️do not store passwords here❗️
namespace="hmpps-person-record-dev"
dev_db_port="5432"
dev_db_remote_host="cloud-platform-7fbd2d1d66465b8d.cdwm328dlye6.eu-west-2.rds.amazonaws.com"
dev_port_forward_pod="port-forward-${USER//./-}"

function on_complete() {
  kubectl \
    --namespace="$namespace" \
    delete "pod/$dev_port_forward_pod"
  exit
}
trap 'on_complete 2> /dev/null' SIGTERM SIGINT

kubectl \
  --namespace="$namespace" \
  run "$dev_port_forward_pod" --image=ministryofjustice/port-forward \
    --port=5432 --env="REMOTE_HOST=$dev_db_remote_host" --env="LOCAL_PORT=5432" --env="REMOTE_PORT=5432"

kubectl \
  --namespace="$namespace" \
  wait --for=condition=ready pod "$dev_port_forward_pod"

echo
echo "✨ Turning on port-forwarding to $(tput setaf 2)$namespace$(tput sgr 0)"
echo "✨ Use $(tput setaf 3)Ctrl-C$(tput sgr 0) to exit and cleanup"
echo "🧑‍💻 Connect to the database via localhost:$dev_db_port and $namespace postgres credentials"
echo

kubectl \
  --namespace="$namespace" \
  port-forward "$dev_port_forward_pod" "$dev_db_port:5432"

