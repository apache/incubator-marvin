import airflow
from airflow import DAG
from airflow.operators.bash_operator import BashOperator
from datetime import timedelta

HOST = "{{cookiecutter.host}}"

args = {
    'owner': '{{cookiecutter.owner}}',
}

dag = DAG(
    dag_id='{{cookiecutter.interval_days}}',
    default_args=args,
    schedule_interval=timedelta(days={{cookiecutter.interval_days}}),
    dagrun_timeout=timedelta(minutes={{cookiecutter.timeout}})
)

acquisitor = BashOperator(
    task_id='acquisitor',
    bash_command='marvin-api acquisitor --wait --host $HOST',
    retries={{cookiecutter.retries}},
    env={
        'HOST': HOST
    },
    dag=dag)

tpreparator = BashOperator(
    task_id='tpreparator',
    bash_command='marvin-api tpreparator --wait --host $HOST',
    retries={{cookiecutter.retries}},
    env={
        'HOST': HOST
    },
    dag=dag)

trainer = BashOperator(
    task_id='trainer',
    bash_command='marvin-api trainer --wait --host $HOST',
    retries={{cookiecutter.retries}},
    env={
        'HOST': HOST
    },
    dag=dag)

evaluator = BashOperator(
    task_id='evaluator',
    bash_command='marvin-api evaluator --wait --host $HOST',
    retries={{cookiecutter.retries}},
    env={
        'HOST': HOST
    },
    dag=dag)