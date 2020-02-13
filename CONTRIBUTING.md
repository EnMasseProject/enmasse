# Contributing Guidelines

## How to contribute to EnMasse

We'd love to accept your patches! Since we **:heart::heart: LOVE :heart::heart:** Contributors and Contributions :smile:

You can start contributing to EnMasse by following the below guidelines:

We assume you already know about git, like resolving merge conflicts, squashing, setting remote etc.

### Getting Source Code

Get the source code by doing a fork and then using the command below:

```
git clone https://github.com/your_github_username/enmasse.git
```

Follow [build instructions](HACKING.md) for building and testing.

### Starting Development

Now you can start your contribution work. See [development guidelines](DEVELOPMENT.md) for how we
like EnMasse code to look like.

#### Finding the issue

There are lots of issues on EnMasse's [issue page](https://github.com/enmasseproject/enmasse/issues). Please go through the issues and find a one which you want to fix/develop. If you want to implement something which is not there, please create a new issue. Please assign that new issue or already existing issue to yourself otherwise it may happen that someone else will fix the same issue. We will try to label issues that makes it easy to get involved in the project with `good-start`.

#### Creating a new branch

Please create a new branch to start your development work. You can create the branch by any name but we will suggest you consider the naming convention like `issue-issueNumber`. Example: `issue-123`.

```
git checkout -b issue-123
```

#### Create your patch

Do all your development or fixing work here.

#### Adding Unit and Regression Tests 

After all your development/fixing work is done, do not forget to add unit tests and system tests (if applicable). It will be nice if you can add an example of the new feature you have added.

#### Check your work after running tests

You should run all the unit tests by hitting the following command

```
make
```

To run systemtests tests, you need to run a OpenShift/Kubernetes Cluster and after that

```
make systemtests
```

#### Other Requirements

* If adding a new feature or fixing some bug please update the [CHANGELOG.md](https://github.com/enmasseproject/enmasse/blob/master/CHANGELOG.md),
* Make sure you add the license headers at top of every new source file you add while implementing the feature (Apache V2.0).

#### Commit your work

After all your work is done, you need to commit the changes.

```
git commit -am "Commit-Message"
```

Please add a very elaborative commit message for the work you have done. It will help the reviewer to understand the things quickly.

#### Rebase the PR

It may happen that during the development, someone else submitted a PR and that is merged. You need to rebase your branch with current upstream master.

#### Push the changes to your fork
 
```
git push origin issue-123
```
 
#### Create a Pull Request

Please create a Pull Request from GitHub to enmasse: master. Please provide a very brief Title and description of PR. Link the PR to issue by adding `#issueNumber` at the end of the description.

### PR Review

Your PR will get reviewed soon from the maintainers of the project. If they suggest changes, do all the changes, commit the changes, rebase the branch, squash the commits and push the changes. If all will be fine, your PR will be merged.

That's it! Thank you for your contribution!

Feel free to suggest changes to this documentation. If you want to discuss something with maintainers, you can ask us on [Gitter](https://gitter.im/EnMasseProject/community) or via a GitHub [issue](https://github.com/enmasseproject/enmasse/issues)

### Note
 
Contributions can be small or big, it does not matter. We even love to receive a typo fix PR. Adding feature or fixing a bug is not the only way to contribute. You can send us PR for adding documentation, fixing typos or adding tests.
