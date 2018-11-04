FROM vertx/vertx3-alpine

# Set the location of the verticles
ENV VERTICLE_HOME /usr/verticles

# Set the name of the verticle to deploy
ENV VERTICLE_NAME sb001.miniturbo.vertx.MiniTurboVerticle

# Set vertx option
ENV VERTX_OPTIONS ""

###
# The rest of the file should be fine.
###

COPY ./verticles $VERTICLE_HOME

# We use the "sh -c" to turn around https://github.com/docker/docker/issues/5509 - variable not expanded
ENTRYPOINT ["sh", "-c"]

if [[ -z "${VERTX_CONFIG}" ]]; then
	CMD ["exec vertx run $VERTICLE_NAME -cp $VERTICLE_HOME/* $VERTX_OPTIONS"]
else
	CMD ["exec vertx run $VERTICLE_NAME -config $VERTX_CONFIG -cp $VERTICLE_HOME/* $VERTX_OPTIONS"]
fi