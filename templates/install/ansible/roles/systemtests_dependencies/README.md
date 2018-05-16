systemtests_dependencies
=========
role contains tasks for dependencies, browsers and clients for frunning full systemtests framework

Role Variables
--------------
- nodejs_version

Tags
--------------

- dependencies
- clients
- selenium
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
$ ansible-playbook systemtests-dependencies.yml --tags clients --skip-tags dependencies
```
