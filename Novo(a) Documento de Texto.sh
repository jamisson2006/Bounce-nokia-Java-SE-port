#!/bin/bash

REPO_URL="https://github.com/jamisson2006/Bounce-nokia-Java-SE-port.git"
BRANCH="main"
COMMIT_MSG="Primeiro commit - codigo fonte Bounce"

echo "Configurando usuario..."

git config --global user.name "jamisson2006"
git config --global user.email "js292879@gmail.com"

echo "Iniciando repositorio..."
git init

if [ ! -f .gitignore ]; then
    echo "bin/" >> .gitignore
    echo ".settings/" >> .gitignore
fi

git add .
git commit -m "$COMMIT_MSG"

git branch -M $BRANCH

git remote remove origin 2> /dev/null
git remote add origin $REPO_URL

echo "Fazendo push..."
git push -u origin $BRANCH