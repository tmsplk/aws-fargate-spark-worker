FROM public.ecr.aws/amazonlinux/amazonlinux

RUN yum -y install java-11-amazon-corretto && \
    yum clean all && \
    rm -rf /var/cache/yum

ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"

COPY target/scala-2.12/aws-fargate-spark-worker.jar /app/

WORKDIR /app

ENTRYPOINT ["java", "-cp", "aws-fargate-spark-worker.jar", "git.tmsplk.aws-fargate-spark-worker.Main"]