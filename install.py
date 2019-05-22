#! /usr/bin/python

import os, sys, traceback, subprocess

DIR=os.path.abspath(os.path.dirname(__file__))

ADB=os.getenv('ADB', 'adb')

APE_ROOT='/data/local/tmp/'



def run_cmd(*args):
    print('Run cmd: ' + (' '.join(*args)))
    subprocess.check_call(*args)


if __name__ == '__main__':
    try:
        SERIAL=os.getenv('SERIAL')
        if SERIAL:
            run_cmd([ADB, '-s', SERIAL, 'push', os.path.join(DIR, 'ape.jar'), APE_ROOT])
            run_cmd([ADB, '-s', SERIAL, 'push', os.path.join(DIR, 'ape'), APE_ROOT])
            run_cmd([ADB, '-s', SERIAL, 'shell', 'chmod', '777', APE_ROOT + '/ape'])
        elif len(sys.argv) > 1 and sys.argv[1] == '-d':
            run_cmd([ADB, '-d', 'push', os.path.join(DIR, 'ape.jar'), APE_ROOT])
            run_cmd([ADB, '-d', 'push', os.path.join(DIR, 'ape'), APE_ROOT])
            run_cmd([ADB, '-d', 'shell', 'chmod', '777', APE_ROOT + '/ape'])
        else:
            run_cmd([ADB, 'push', os.path.join(DIR, 'ape.jar'), APE_ROOT])
            run_cmd([ADB, 'push', os.path.join(DIR, 'ape'), APE_ROOT])
            run_cmd([ADB, 'shell', 'chmod', '777', APE_ROOT + '/ape'])
    except:
        traceback.print_exc()

