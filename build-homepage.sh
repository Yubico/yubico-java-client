#!/bin/sh

set -e

STAGING_DIR=`mktemp -d '/tmp/yubico-j-doc.XXXXXX'`
asciidoc -s -o $STAGING_DIR/README.html README
asciidoc -s -o $STAGING_DIR/jaas.html jaas/README
asciidoc -s -o $STAGING_DIR/v1client.html v1client/ReadMe.txt
asciidoc -s -o $STAGING_DIR/v2client.html v2client/README

mvn javadoc:aggregate
cp -r target/site/apidocs $STAGING_DIR/apidocs

git checkout gh-pages
cat index.html.in $STAGING_DIR/README.html > index.html
echo "</div></body></html>" >> index.html
cat jaas.html.in $STAGING_DIR/jaas.html > jaas.html
echo "</div></body></html>" >> jaas.html
cat v1client.html.in $STAGING_DIR/v1client.html > v1client.html
echo "</div></body></html>" >> v1client.html
cat v2client.html.in $STAGING_DIR/v2client.html > v2client.html
echo "</div></body></html>" >> v2client.html

rm -rf apidocs
cp -r $STAGING_DIR/apidocs apidocs
rm -rf $STAGING_DIR

git add index.html
git add jaas.html
git add v1client.html
git add v2client.html
git commit -m "updated page with new README"
git add apidocs
git commit -m "updated javadocs"
git checkout master
