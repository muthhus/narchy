'use strict';

const app = require('app');
const BrowserWindow = require('browser-window');

let mainWindow = null;

app.on('ready', function() {
    mainWindow = new BrowserWindow({
        title: 'NARchy',
        height: 900,
        width: 1400,
        'node-integration': false,
        autoHideMenuBar: true,
        darkTheme: true
    });

    mainWindow.loadURL('file://' + __dirname + '/index.html');
});