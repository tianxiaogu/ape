#! /usr/bin/python

import os, sys, traceback, subprocess

ADB=os.getenv('ADB', 'adb')

APE_ROOT='/sdcard/'
APE_JAR=APE_ROOT + 'ape.jar'

APE_MAIN='com.android.commands.monkey.Monkey'

APP_PROCESS='/system/bin/app_process'

BASE_CMD=[ADB, 'shell', 'CLASSPATH=' + APE_JAR, APP_PROCESS, APE_ROOT, APE_MAIN]

def run_cmd(*args):
    print('Run cmd: ' + (' '.join(*args)))
    subprocess.check_call(*args)

def run_ape(*args):
    run_cmd(BASE_CMD + list(args))

if __name__ == '__main__':
    try:
        run_ape(*sys.argv[1:])
    except:
        traceback.print_exc()

