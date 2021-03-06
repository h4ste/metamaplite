#!/bin/sh
#
# A simplified metamaplite bash script using metamaplite standalone
# jar, should work on MINGW bash (and GIT bash) on Windows.
# 

PROJECTDIR=$(dirname $0)

OPENNLP_MODELS=$PROJECTDIR/data/models
CONFIGDIR=$PROJECTDIR/config

MML_JVM_OPTS=-Xmx12g

# metamaplite properties
MMLPROPS="-Dopennlp.en-sent.bin.path=$OPENNLP_MODELS/en-sent.bin \
    -Dopennlp.en-token.bin.path=$OPENNLP_MODELS/en-token.bin \
    -Dopennlp.en-pos.bin.path=$OPENNLP_MODELS/en-pos-perceptron.bin \
    -Dopennlp.en-chunker.bin.path=$OPENNLP_MODELS/en-chunker.bin \
    -Dlog4j.configurationFile=$PROJECTDIR/config/log4j2.xml \
    -Dmetamaplite.entitylookup.resultlength=1500 \
    -Dmetamaplite.ivf.cuiconceptindex=$PROJECTDIR/data/ivf/2017AA/USAbase/strict/indices/cuiconcept \
    -Dmetamaplite.ivf.firstwordsofonewideindex=$PROJECTDIR/data/ivf/2017AA/USAbase/strict/indices/first_words_of_one_WIDE \
    -Dmetamaplite.ivf.cuisourceinfoindex=$PROJECTDIR/data/ivf/2017AA/USAbase/strict/indices/cuisourceinfo \
    -Dmetamaplite.ivf.cuisemantictypeindex=$PROJECTDIR/data/ivf/2017AA/USAbase/strict/indices/cuist \
    -Dmetamaplite.ivf.varsindex=$PROJECTDIR/data/ivf/2017AA/USAbase/strict/indices/vars \
    -Dmetamaplite.ivf.meshtcrelaxedindex=$PROJECTDIR/data/ivf/2017AA/USAbase/strict/indices/meshtcrelaxed \
    -Dmetamaplite.excluded.termsfile=$PROJECTDIR/data/specialterms.txt"

CLASSPATH=$CONFIGDIR
java $MML_JVM_OPTS $MMLPROPS -cp $PROJECTDIR/target/metamaplite-*-standalone.jar gov.nih.nlm.nls.ner.MetaMapLite $* 

