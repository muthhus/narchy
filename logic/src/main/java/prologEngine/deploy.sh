mkdir prologEngine
cp *.java *.pl *.sh *.md prologEngine
mkdir prologEngine/progs
cp progs/* prologEngine/progs
zip -r prologEngine.zip prologEngine
mv -f prologEngine.zip $HOME/Desktop
rm -r -f prologEngine