import { defineStore } from "pinia";
import { ref } from "vue";
import { getLoginUserUsingGet } from '@/api/userController.ts'

/**
 * 登录用户信息
 */
export const useLoginUserStore = defineStore("loginUser", () => {
  const loginUser = ref<API.LoginUserVO>({
    userName: "未登录",
  });

  /**
   * 获取登录用户信息
   */
  async function fetchLoginUser() {
    // todo 由于后端还没提供接口，暂时注释
    const res = await getLoginUserUsingGet();
    if (res.data.code === 0 && res.data.data) {
      loginUser.value = res.data.data;
    }
    // 测试用户登录，3秒后自动登录
    // setTimeout(() => {
    //   loginUser.value = { userName: "测试用户", id: 1 };
    // }, 3000);
  }

  function setLoginUser(newLoginUser: any) {
    loginUser.value = newLoginUser;
  }

  return { loginUser, setLoginUser, fetchLoginUser };
});
