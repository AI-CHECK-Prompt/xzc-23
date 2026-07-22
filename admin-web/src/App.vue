<template>
  <el-container class="layout">
    <el-aside width="220px" class="sidebar">
      <div class="logo">渔船渔获管控</div>
      <el-menu :default-active="route.path" router>
        <el-menu-item index="/dashboard">
          <el-icon><DataBoard /></el-icon><span>总览</span>
        </el-menu-item>
        <el-menu-item index="/vessel">
          <el-icon><Ship /></el-icon><span>船舶档案</span>
        </el-menu-item>
        <el-menu-item index="/voyage">
          <el-icon><Document /></el-icon><span>出海申报</span>
        </el-menu-item>
        <el-menu-item index="/catch">
          <el-icon><Box /></el-icon><span>渔获与过磅</span>
        </el-menu-item>
        <el-menu-item index="/alert">
          <el-icon><Warning /></el-icon><span>告警事件</span>
        </el-menu-item>
        <el-menu-item index="/quota">
          <el-icon><Histogram /></el-icon><span>配额管控</span>
        </el-menu-item>
        <el-menu-item index="/enforcement">
          <el-icon><Stamp /></el-icon><span>执法办案</span>
        </el-menu-item>
        <el-menu-item index="/purchase">
          <el-icon><Money /></el-icon><span>采购回传</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <span class="title">福建省近海小型渔船渔获登记与配额管控平台</span>
        <el-tag type="success" effect="dark">运行中</el-tag>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRoute } from 'vue-router'
const route = useRoute()
</script>

<style>
html, body, #app { margin: 0; padding: 0; height: 100%; font-family: 'Helvetica Neue', Arial, 'PingFang SC', 'Microsoft YaHei', sans-serif; }
.layout { height: 100vh; }
.sidebar { background: #001529; color: #fff; }
.logo { color: #fff; padding: 18px; text-align: center; font-weight: 700; font-size: 16px; border-bottom: 1px solid #1f3a5a; }
.el-menu { background: #001529; border: 0; }
.el-menu-item { color: #c9d1d9; }
.el-menu-item.is-active { background: #1890ff; color: #fff; }
.topbar { background: #fff; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #f0f0f0; }
.title { font-size: 16px; font-weight: 600; }
.el-main { background: #f4f6fa; }
</style>
