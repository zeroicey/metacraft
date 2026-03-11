import {
  PersonalInfo,
  SwitchInfo,
  ApiHandler,
  GenderType
} from 'collect_personal_info';

export function createProfileSwitchInfo(): SwitchInfo {
  return {
    showAvatar: true,
    showNickName: true,
    showFullName: true,
    showGender: true,
    showBirthDate: true,
    showPhoneNumber: true,
    showIntroDesc: true,
  };
}

export function createProfilePersonalInfo(): PersonalInfo {
  return {
    avatar: 'https://agc-storage-drcn.platform.dbankcloud.cn/v0/news-hnp2d/avatar%2Favatar_12.jpg',
    nickName: '华为用户',
    gender: GenderType.MALE,
    birthDate: new Date('1990-10-01'),
    fullName: '张三',
    phoneNumber: '10000001111',
    introDesc: '在自己的小世界里,做一个快乐的普通人,把节奏放慢,享受生活!十六岁的青春张扬 十八岁的未来可期,携零零碎碎的美好奔赴未知的将来!',
  };
}

function createResolvedPromise(): Promise<void> {
  return new Promise((resolve) => resolve(undefined));
}

function createDelayedResolvedPromise(): Promise<void> {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(undefined);
    }, 1000);
  });
}

export function createProfileApiHandler(): ApiHandler {
  return {
    updateAvatar: (_param: Object) => createResolvedPromise(),
    updateNickName: (_param: Object) => createResolvedPromise(),
    updateGender: (_param: Object) => createResolvedPromise(),
    updateBirth: (_param: Object) => createResolvedPromise(),
    updateFullName: (_param: Object) => createResolvedPromise(),
    updateIntro: (_param: Object) => createResolvedPromise(),
    updatePhoneInfo: {
      codeLength: 6,
      sendCode: (_phone: string) => createDelayedResolvedPromise(),
      verifyCode: (_phone: string, _code: string) => createDelayedResolvedPromise(),
    },
  };
}