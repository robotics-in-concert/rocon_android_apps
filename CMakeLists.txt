##############################################################################
# CMake
##############################################################################

cmake_minimum_required(VERSION 2.8.3)
project(rocon_android_apps)

##############################################################################
# Catkin
##############################################################################

find_package(catkin REQUIRED rosjava_build_tools)
catkin_android_setup(assembleRelease uploadArchives)
catkin_package()

install(DIRECTORY ${CATKIN_DEVEL_PREFIX}/${CATKIN_GLOBAL_MAVEN_DESTINATION}/com/github/robotics_in_concert/rocon_android_apps/
        DESTINATION ${CATKIN_GLOBAL_MAVEN_DESTINATION}/com/github/robotics_in_concert/rocon_android_apps/)