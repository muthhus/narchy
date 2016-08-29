./clean.sh
export T_STAMP=`date "+%Y_%m_%d_%H"`
pushd .
cd ../
zip -r npe_$T_STAMP.zip src
mv -f npe_$T_STAMP.zip  $HOME/Dropbox/Work/bak/
ls -l $HOME/Dropbox/Work/bak/npe_$T_STAMP.zip
popd


