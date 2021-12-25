# TravelMapAOS

[**데모 보러가기**](https://drive.google.com/file/d/1kNGwE8Wv2xIOs4Mjw4Pl2KC4rPVGINqc/view?usp=sharing)

## 기획의도
1. 사진을 찍는 것 만으로는 그 여행에 대한 기록을 남기기 힘들고 사진을 촬영한 장소를 모두 기억하는 것은 상당히 어려울 것이다.\
 -> **사진이 촬영한 장소를 저장해주며 여행한 경로를 관리해주는 서비스가 있다면 해결되지 않을까?**
2. 여행한 사진을 공유하고자 한다면 많은 사진을 공유해야 하거나 여러 개의 SNS의 글을 공유해야한다.\
 -> **사용자가 여행한 흔적을 한 페이지로 압축할 수 있다면 좋지 않을까?**
 
## 기술스택
  **```앱 플랫폼 : AOS(Kotlin), IOS(Swift)```**\
  **```서버의 프레임워크 : Spring(Kotlin)```**\
  **```DB : MariaDB```**
  
## 기능설명

### 갤러리 기능
> AOS/IOS앱 모두 평소에는 갤러리로 사용할 수 있습니다.\
> 사진은 앨범 별 혹은 날짜 별로 분류가 되어있습니다.\
> 또한 그에 더해 기기내 모든 사진의 메타데이터를 분석하여 좌표정보를 \
> 추출하고 지도위에 각 사진들을 올리는 기능 또한 구현하였습니다. 
> 
> <img src="https://user-images.githubusercontent.com/44064530/147388678-afeeabe9-948c-4846-9ce1-2e24624e0d3d.jpg" width="30%" height="30%"> <img src="https://user-images.githubusercontent.com/44064530/147388693-9836e6e2-1e17-41af-a139-8a781601bef1.jpg" width="30%" height="30%"> <img src="https://user-images.githubusercontent.com/44064530/147388694-ea244c7c-c530-4e75-ae38-cc7c83509629.jpg" width="30%" height="30%">

### 여행 기록 기능

> “여행”화면에서는 여행 기록의 시작/중지, 일시 중지/재시작, 기록속도 등을 설정할 수 있습니다.\
> 사용자가 여행이 끝났다고 판단하면 기록을 끝내고 여행을 잘 기억 할 수 있도록 별명을 지어줄 수 있습니다.
> 
> <img src="https://user-images.githubusercontent.com/44064530/147388710-b9497435-6362-446f-a46d-8abc606ac8ed.jpg" width="30%" height="30%"> <img src="https://user-images.githubusercontent.com/44064530/147388708-f31c2bff-cfd8-4071-b1f8-c296fd5e192c.jpg" width="30%" height="30%">
> 
> “여행”화면에서는 “여행 기록 리스트” 버튼을 눌러 사용자가 지정한 별명으로 여행기록들을 확인할 수 있습니다.\
> 사용자가 원하는 여행기록을 누르면 여행 갔던 경로와 각 장소에서 찍었던 사진들을 애니메이션으로 확인할 수 있습니다.\
> 여행기록을 길게 누르면 메뉴 창이 나오고 이 창을 통해 삭제, 별명변경, 여행기록 공유를 할 수 있습니다.
> 
> <img src="https://user-images.githubusercontent.com/44064530/147388730-e64f5047-0fee-46cc-8ba4-633ba9538465.jpg" width="30%" height="30%"> <img src="https://user-images.githubusercontent.com/44064530/147388729-52d3cce8-5c76-4a1c-8343-2c7f4fafba83.jpg" width="30%" height="30%">   <img src="https://user-images.githubusercontent.com/44064530/147388727-df997e90-0698-4859-a50c-fda698454876.jpg" width="30%" height="30%">

### 여행 기록 공유

> 여행기록 리스트에서 여행 공유
를 누르면 사용자가 원하는 범위와 공유 품질을 선택하여 공유할 수 있습니다.\
> 암호화로 여행기록을 공유하는 경우 단순히 사용자가 입력한 비밀번호가 파일의 암호화키가 되는 것이 아닌\
> 임의로 생성된 Salt와 함께 Key Strtching 알고리즘을 통해 암호화 키를 만들어서 사용하기 때문에 무차별 대입 공격에 강합니다.
> 
> <img src="https://user-images.githubusercontent.com/44064530/147388744-21a589a6-2fed-4195-9da6-6c910c638d11.jpg" width="30%" height="30%"> <img src="https://user-images.githubusercontent.com/44064530/147388743-8d95c38f-de78-41bb-ad9b-1a0882f4f656.jpg" width="30%" height="30%">
> 
> 사용자가 여행기록을 공유하고 난 뒤에는 여행 메뉴는 공유취소, 공유 링크 가져오기가 추가되고\
> 공유 링크 가져오기를 통해 공유 링크를 발급받을 수 있습니다.
> 
> <img src="https://user-images.githubusercontent.com/44064530/147388765-b05fad40-524d-43af-a14f-c28588d3dc55.jpg" width="30%" height="30%"> <img src="https://user-images.githubusercontent.com/44064530/147388764-19eebbb2-4255-41c9-98ae-0bd67982865e.jpg" width="30%" height="30%">

### 소셜 로그인
> 사용자가 여행기록을 공유하거나 반대로 타인의 여행기록을 공유 받고자 할 때는 소셜 로그인을 통해서 로그인한 뒤에 가능합니다.\
> 소셜 로그인은 Firebase Auth를 이용하여 구현 되었으며 구글, 애플, 페이스북, 트위터 이 4개의 공급자가 사용되었습니다.
> 
> <img src="https://user-images.githubusercontent.com/44064530/147388773-d27e9c69-b35c-4532-9735-8ef14305be7e.jpg" width="30%" height="30%">
> <img src="https://user-images.githubusercontent.com/44064530/147388772-0ff1722a-b01a-401d-afcb-86902dd991e5.png" width="100%" height="100%">

### 친구 신청
> 친구 리스트를 확인할 수 있고 다른 사용자의 닉네임을 알고 있다면 닉네임을 \
> 통해 친구 신청을 할 수도 있습니다. 회색 줄은 친구 신청을 했지만 받아들여지지 않은 신청이거나\
> 반대로 받았지만, 아직 수락하지 않은 친구 신청 상태를 의미합니다.
> 
> <img src="https://user-images.githubusercontent.com/44064530/147388785-e37d21e5-afc8-451e-8691-8addd43238bb.jpg" width="30%" height="30%"> <img src="https://user-images.githubusercontent.com/44064530/147388786-632d0a72-195f-430c-a419-1b1187108139.jpg" width="30%" height="30%"> <img src="https://user-images.githubusercontent.com/44064530/147388784-3cfa362f-391b-4f33-88a3-d248d5411410.jpg" width="30%" height="30%">

### 여행 기록 열람
> 앱을 설치하지 않아도 공유 링크만 있다면 브라우저를 통해 여행기록을 열람할 수 있습니다.
> 
> <img src="https://user-images.githubusercontent.com/44064530/147388832-1d4c33af-76f8-4f36-ac51-056156babb36.jpg" width="30%" height="30%">
> <img src="https://user-images.githubusercontent.com/44064530/147388830-042b0c7b-62bd-4ead-804a-517e2edcfbf4.png" width="100%" height="100%">
> 
> 여행을 공유한 사용자와 친구가 아니라면 친구 신청을 유도합니다.
> 
> <img src="https://user-images.githubusercontent.com/44064530/147388834-050a0754-b095-456d-856f-1ef88d4a9fad.jpg" width="30%" height="30%">
> <img src="https://user-images.githubusercontent.com/44064530/147388840-458a91a5-6f60-41b9-af78-754b5cd5a8ed.png" width="100%" height="100%">
> 
> 브라우저의 성능 한계로 인하여 암호화된 여행기록의 경우에는 앱에서만 열람할 수 있습니다.
> 
> <img src="https://user-images.githubusercontent.com/44064530/147388836-9b712696-7cd9-432c-b1c8-c31479be8330.jpg" width="30%" height="30%">
> <img src="https://user-images.githubusercontent.com/44064530/147388864-71cbbf2b-7bb2-4a17-9e3b-a53917f40f06.png" width="100%" height="100%">
