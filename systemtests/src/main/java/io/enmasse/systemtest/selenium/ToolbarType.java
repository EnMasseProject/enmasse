package io.enmasse.systemtest.selenium;

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
