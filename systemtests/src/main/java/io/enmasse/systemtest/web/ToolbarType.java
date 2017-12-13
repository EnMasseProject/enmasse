package io.enmasse.systemtest.web;

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
