systemtests_dependencies
=========
role contains tasks for dependencies, browsers and clients for frunning full systemtests framework

Role Variables
--------------

- geckodriver_version
- chromedriver_version
- nodejs_version
- firefox_version

Tags
--------------

- dependencies
- clients
- webdrivers
- firefox

Example Playbook
----------------
#### Including all tasks
      roles:
         - { role: systemtests_dependencies, become: yes}

#### Specific task
      roles:
         - { role: systemtests_dependencies, become: yes, tags: ['clients']}

#### Call specific from command line
```sh
$ ansible-playbook playbook-name.yml --tags clients --skip-tags dependencies
```
