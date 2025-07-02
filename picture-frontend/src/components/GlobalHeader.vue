<template>
  <div id="globalHeader">
    <a-row :wrap="false">
      <a-col flex="200px">
        <router-link to="/">
          <div class="title-bar">
            <img class="logo" src="../assets/logo.png" alt="logo"/>
            <div class="title">共享图片云平台</div>
          </div>
        </router-link>
      </a-col>
      <a-col flex="auto">
        <a-menu v-model:selectedKeys="current" mode="horizontal" :items="items" @click="doMenuClick"/>
      </a-col>
      <!--用户信息展示栏-->
      <a-col flex="120px">
        <div class="user-login-status">
          <div v-if="loginUserStore.loginUser.id">
            <a-dropdown>
              <ASpace>
                <a-avatar :src="loginUserStore.loginUser.userAvatar" />
                {{ loginUserStore.loginUser.userName ?? '无名' }}
              </ASpace>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="doLogout">
                    <LogoutOutlined />
                    退出登录
                  </a-menu-item>
                  <a-menu-item>
                    <router-link to="/my_space">
                      <UserOutlined />
                      我的空间
                    </router-link>
                  </a-menu-item>
                  <a-menu-item @click="doPaste">
                      <share-alt-outlined />
                      复制UID
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>

          <div v-else>
            <a-button type="primary" href="/user/login">登录</a-button>
          </div>
        </div>
      </a-col>
    </a-row>
  </div>
</template>
<script lang="ts" setup>
import { computed, h, ref } from 'vue'
import { HomeOutlined } from '@ant-design/icons-vue'
import { MenuProps, message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { userLogoutUsingPost } from '@/api/userController.ts'
import { LogoutOutlined, UserOutlined} from '@ant-design/icons-vue'
import AddSpacePage from '@/pages/AddSpacePage.vue'
import { ShareAltOutlined } from '@ant-design/icons-vue'
const loginUserStore = useLoginUserStore()

const originItems = [
  {
    key: '/',
    icon: () => h(HomeOutlined),
    label: '主页',
    title: '主页',
  },
  {
    key: '/admin/userManage',
    label: '用户管理',
    title: '用户管理',
  },
  {
    key: '/add_picture',
    label: '编辑图片',
    title: '编辑图片',
  },
  {
    key: '/admin/pictureManage',
    label: '图片管理',
    title: '图片管理',
  },
  {
    key: '/admin/spaceManage',
    label: '空间管理',
    title: '空间管理',
  },
  {
    key: '/',
    label: h('a', { href: 'https://tv.cctv.com/cctv14/', target: '_blank' }, '🚀'),
    title: '🚀',
  },


]

// 过滤菜单项
const filterMenus = (menus = []) => {
  return menus?.filter((menu) => {
    // 先检查 menu.key 是否存在
    if (menu.key && menu.key.startsWith("/admin")) {
      const loginUser = loginUserStore.loginUser;
      if (!loginUser || loginUser.userRole !== "admin") {
        return false;
      }
    }
    return true;
  });
};

// 展示在菜单的路由数组
const items = computed<MenuProps['items']>(() => filterMenus(originItems))


const router = useRouter();
//路由跳转事件
const doMenuClick = ({ key }: { key: string }) => {
  router.push({
    path: key
  })
}

// 当前要高亮的菜单项
const current = ref<string[]>([])
// 监听路由变化，更新菜单项
router.afterEach((to, from, next) => {
  current.value=[to.path]
})
// 用户注销
const doLogout = async () => {
  const res = await userLogoutUsingPost()
  console.log(res)
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}

const copyUserIdToClipboard = async () => {
  if (!loginUserStore.loginUser?.id) {
    return false;
  }
  const userId = loginUserStore.loginUser.id.toString();
  await navigator.clipboard.writeText(userId);
  return true;
};
//复制UID
const doPaste = async () => {
  if (await copyUserIdToClipboard()) {
    message.success('用户ID已复制');
  } else {
    message.error('复制失败');
  }
};
</script>

<style scoped>
#globalHeader .title-bar{
  display: flex;
  align-items: center;

}

.title{
  color: black;
  font-size: 18px;
  margin-left: 16px;
}

.logo{
  height: 48px;
}
</style>


