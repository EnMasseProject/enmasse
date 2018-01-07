package io.enmasse.systemtest.TestCollection.web;

public enum ToolbarType {
    ADDRESSES{
        public String toString(){
            return "exampleToolbar";
        }
    },
    CONNECTIONS{
        public String toString(){
            return "connectionToolbar";
        }
    };
}
