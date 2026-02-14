cmake_minimum_required(VERSION 3.22.1)
project("speakalert")
add_library(speakalert SHARED src/main/cpp/dummy.cpp)
target_link_options(speakalert PRIVATE "-Wl,-z,max-page-size=16384")
